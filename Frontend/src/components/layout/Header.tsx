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
    <header className="h-14 bg-white border-b border-[#E8EAEE] sticky top-0 z-20 flex items-center justify-between px-4 lg:px-6 select-none flex-shrink-0">
      {/* Desktop Left Breadcrumb */}
      <div className="hidden lg:flex items-center gap-3">
        {showBack && (
          <button 
            onClick={() => navigate('/')} 
            className="w-8 h-8 rounded-lg flex items-center justify-center text-[#5E6278] hover:bg-slate-100 hover:text-slate-900 transition-colors cursor-pointer"
          >
            <ArrowLeft size={18} />
          </button>
        )}
        <div className="flex items-center gap-2 text-slate-700">
          <ClipboardList size={18} className="text-slate-500" />
          <span className="text-sm font-semibold tracking-tight text-slate-800">{title}</span>
        </div>
      </div>

      {/* Mobile Left Bar */}
      <div className="flex lg:hidden items-center gap-2.5">
        {showBack && (
          <button 
            onClick={() => navigate('/')}
            className="w-8 h-8 rounded-lg flex items-center justify-center text-slate-800 hover:bg-slate-100"
          >
            <ArrowLeft size={19} />
          </button>
        )}
        <div className="flex items-center gap-2">
          <div className="w-7 h-7 bg-[#16171D] rounded-lg flex items-center justify-center shadow-xs">
            <span className="text-white font-bold text-sm leading-none">V</span>
          </div>
          <span className="text-slate-900 font-bold text-lg tracking-tight">VedaAI</span>
        </div>
      </div>

      {/* Right Actions & User Profile */}
      <div className="flex items-center gap-3">
        {/* Help Circle Button */}
        <button 
          title="Help & Support"
          className="hidden lg:flex w-8 h-8 rounded-full border border-slate-200/90 items-center justify-center text-slate-500 hover:bg-slate-50 hover:text-slate-800 transition-colors cursor-pointer"
        >
          <HelpCircle size={16} />
        </button>

        {/* Notification Bell */}
        <button 
          title="Notifications"
          className="w-8 h-8 rounded-full flex items-center justify-center text-slate-500 hover:bg-slate-50 hover:text-slate-800 relative transition-colors cursor-pointer"
        >
          <Bell size={18} />
          <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-[#F27448] rounded-full ring-2 ring-white" />
        </button>

        {/* Sparkle AI Icon */}
        <button 
          title="AI Assistant"
          className="hidden lg:flex w-8 h-8 rounded-full items-center justify-center text-[#F27448] hover:bg-orange-50 transition-colors cursor-pointer"
        >
          <Sparkles size={18} />
        </button>

        {/* User Profile for Lalit Adhikari */}
        <div className="flex items-center gap-2.5 pl-2 cursor-pointer group">
          <div className="relative">
            <div className="w-8 h-8 rounded-full bg-gradient-to-br from-amber-500 via-rose-500 to-indigo-600 flex items-center justify-center text-white text-xs font-bold ring-1 ring-slate-200 overflow-hidden shadow-2xs">
              <span className="select-none">LA</span>
            </div>
            {/* Status indicator dot */}
            <span className="absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 bg-[#22C55E] rounded-full border-2 border-white flex items-center justify-center" />
          </div>

          <span className="hidden lg:inline text-[13.5px] font-semibold text-slate-800 group-hover:text-slate-900 transition-colors">
            Lalit Adhikari
          </span>
          <ChevronDown size={14} className="hidden lg:inline text-slate-400 group-hover:text-slate-600 transition-colors" />
        </div>

        {/* Mobile Menu Toggle */}
        <button 
          onClick={onMenuToggle}
          className="flex lg:hidden w-8 h-8 rounded-lg items-center justify-center text-slate-800 hover:bg-slate-100 transition-colors ml-1"
        >
          <Menu size={22} />
        </button>
      </div>
    </header>
  );
}
