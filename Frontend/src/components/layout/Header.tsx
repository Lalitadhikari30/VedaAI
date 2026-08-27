import { ArrowLeft, ClipboardList, HelpCircle, Bell, Sparkles, ChevronDown, Menu } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

interface HeaderProps {
  onMenuToggle?: () => void;
  title?: string;
  showBack?: boolean;
}

export default function Header({ onMenuToggle, title = 'Exams', showBack = true }: HeaderProps) {
  const navigate = useNavigate();

  return (
    <header className="h-16 bg-white border-b border-[#ECECEA] sticky top-0 z-20 flex items-center justify-between px-6 select-none flex-shrink-0">
      {/* Desktop Left Breadcrumb */}
      <div className="hidden lg:flex items-center gap-3">
        {showBack && (
          <button 
            onClick={() => navigate('/')} 
            className="w-9 h-9 rounded-xl flex items-center justify-center text-[#5E6278] hover:bg-[#F5F5F3] hover:text-[#1C1C1C] transition-colors cursor-pointer"
          >
            <ArrowLeft size={19} />
          </button>
        )}
        <div className="flex items-center gap-2.5 text-[#1C1C1C]">
          <ClipboardList size={19} className="text-[#E8623C]" />
          <span className="text-[15px] font-bold tracking-tight text-[#1C1C1C]">{title}</span>
        </div>
      </div>

      {/* Mobile Left Bar */}
      <div className="flex lg:hidden items-center gap-3">
        {showBack && (
          <button 
            onClick={() => navigate('/')}
            className="w-9 h-9 rounded-xl flex items-center justify-center text-slate-800 hover:bg-slate-100"
          >
            <ArrowLeft size={19} />
          </button>
        )}
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 bg-[#1C1C1C] rounded-xl flex items-center justify-center shadow-xs">
            <span className="text-white font-bold text-sm leading-none">V</span>
          </div>
          <span className="text-slate-900 font-extrabold text-lg tracking-tight">VedaAI</span>
        </div>
      </div>

      {/* Right Actions & User Profile */}
      <div className="flex items-center gap-3.5">
        {/* Help Circle Button */}
        <button 
          title="Help & Support"
          className="hidden lg:flex w-9 h-9 rounded-full border border-[#E5E5E3] items-center justify-center text-[#6B6B68] hover:bg-[#F5F5F3] hover:text-[#1C1C1C] transition-colors cursor-pointer"
        >
          <HelpCircle size={17} />
        </button>

        {/* Notification Bell */}
        <button 
          title="Notifications"
          className="w-9 h-9 rounded-full flex items-center justify-center text-[#6B6B68] hover:bg-[#F5F5F3] hover:text-[#1C1C1C] relative transition-colors cursor-pointer"
        >
          <Bell size={19} />
          <span className="absolute top-2 right-2 w-2 h-2 bg-[#E8623C] rounded-full ring-2 ring-white" />
        </button>

        {/* Sparkle AI Icon */}
        <button 
          title="AI Assistant"
          className="hidden lg:flex w-9 h-9 rounded-full items-center justify-center text-[#E8623C] bg-orange-50/70 hover:bg-orange-100/70 transition-colors cursor-pointer"
        >
          <Sparkles size={18} />
        </button>

        {/* User Profile for Lalit Adhikari */}
        <div className="flex items-center gap-3 pl-2 cursor-pointer group">
          <div className="relative">
            <div className="w-9 h-9 rounded-full bg-gradient-to-br from-amber-500 via-rose-500 to-indigo-600 flex items-center justify-center text-white text-xs font-bold ring-2 ring-white shadow-xs overflow-hidden">
              <span className="select-none">LA</span>
            </div>
            {/* Online status indicator dot */}
            <span className="absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 bg-[#22C55E] rounded-full border-2 border-white" />
          </div>

          <span className="hidden lg:inline text-[14px] font-bold text-[#1C1C1C] group-hover:text-black transition-colors">
            Lalit Adhikari
          </span>
          <ChevronDown size={15} className="hidden lg:inline text-[#8A8A87] group-hover:text-[#1C1C1C] transition-colors" />
        </div>

        {/* Mobile Menu Toggle */}
        <button 
          onClick={onMenuToggle}
          className="flex lg:hidden w-9 h-9 rounded-lg items-center justify-center text-slate-800 hover:bg-slate-100 transition-colors ml-1"
        >
          <Menu size={22} />
        </button>
      </div>
    </header>
  );
}